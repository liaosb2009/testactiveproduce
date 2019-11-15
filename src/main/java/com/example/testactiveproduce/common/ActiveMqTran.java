package com.example.testactiveproduce.common;

import com.example.testactiveproduce.dao.ActivemqHistoryMapper;
import com.example.testactiveproduce.dao.TestAaMapper;
import com.example.testactiveproduce.model.ActivemqHistory;
import com.example.testactiveproduce.model.TestAa;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ActiveMQMessageProducer;
import org.apache.activemq.AsyncCallback;
import org.apache.activemq.ScheduledMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.jms.*;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/*
 * @ClassName:ActiveMqTran
 * @Description  解决分布式事物
 * @Author liao
 * @Time 2019/9/5 15:52
 */
@Service
@TestAnnotation("")
public class ActiveMqTran {

    private  static final Logger logger = LoggerFactory.getLogger(ActiveMqTran.class);

    private final static String BROKERURL = "failover:(tcp://192.168.137.128:61616,tcp://192.168.137.128:61617,tcp://192.168.137.128:61618)";
    private final static String QUEUE_NAME = "failoverQueue01";
    private volatile Integer counts = 0;

    @Autowired
    private  TestAaMapper testAaMapper;

    @Autowired
    private ActivemqHistoryMapper activemqHistoryMapper;

    @PostConstruct
    public void start(){
        try {
            Method testmysql = ActiveMqTran.class.getMethod("testmysql");
            TestAnnotation annotation = testmysql.getAnnotation(TestAnnotation.class);

            String string = "1111223122";//UUID.randomUUID().toString();

            Integer id=1;
            this.testmysql(id);

            String message=id+":"+System.currentTimeMillis();
            this.testactivemq(string,message);
        } catch (Exception e) {
            logger.error("错误!"+e);
            e.printStackTrace();
        }
    }

    @TestAnnotation("1")
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public boolean testmysql(Integer id) throws Exception {
        try {
            TestAa testAa = new TestAa();
            testAa.setAge("1");
            //testAa.setId(id);
            testAa.setName("testAa");
            testAa.setCreateTime(new Date());
            int i = testAaMapper.insertSelective(testAa);
            if(i==1){
                this.insertHistory(id.longValue());
            }
        } catch ( Exception e) {
            throw new Exception(e);
        }
        return true;
    }

    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public boolean insertHistory(Long uuid) throws Exception {

        try {
            ActivemqHistory activemqHistory = new ActivemqHistory();
            //activemqHistory.setId(1);
            activemqHistory.setUuid(uuid);
            activemqHistory.setCreateTime(new Date());
            activemqHistory.setStatus("1");
            int i = activemqHistoryMapper.insertSelective(activemqHistory);

        } catch ( Exception e) {

            throw new Exception();
        }
        return true;
    }



    @Transactional(propagation= Propagation.REQUIRED,rollbackFor = Exception.class)
    public void testactivemq(String uuid,String message) throws Exception {

        ActiveMQConnectionFactory acMqConnection = new ActiveMQConnectionFactory("tcp://192.168.137.128:61616");
        //开启异步投递
        acMqConnection.setUseAsyncSend(true);
        //设置超时时间
        acMqConnection.setSendTimeout(3000);

        //创建连接工厂
        Connection connection = acMqConnection.createConnection();

        //获得连接
        connection.start();
        //两个参数1：事务2：签收
        /**
         * 当第一个参数为true时，表示使用了事务，现在使用send方法，并不能发送还需要
         * commit提交
         * 解决分布式事务使用手动签收
         */
        Session session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);
        //队列或topic
        //队列
        Queue queue = session.createQueue(QUEUE_NAME);
        //创建消息生产者
        ActiveMQMessageProducer acProducer = (ActiveMQMessageProducer) session.createProducer(queue);
        //持久化
        acProducer.setDeliveryMode(DeliveryMode.PERSISTENT);

        try {
            TextMessage textMessage = session.createTextMessage(message);
            //失败尝试4秒内2次发送,默认1秒6次
            //3秒3次
            textMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_PERIOD,3*1000);
            //共发送3次
            textMessage.setIntProperty(ScheduledMessage.AMQ_SCHEDULED_REPEAT,3);
            //创建消息
            //  textMessage.setStringProperty("c01", "vip");
            //使用mysql,必须开启持久化
            //消息头id,用来处理异步发送确实发送成功
            textMessage.setJMSMessageID(uuid);
            String jmsMessageID = textMessage.getJMSMessageID();

            //异步成功后返回的信息
            acProducer.send(textMessage, new AsyncCallback() {
                @Override
                public void onSuccess() {
                    ActivemqHistory activemqHistory = new ActivemqHistory();
                    activemqHistory.setUuid(Long.parseLong(uuid));
                    activemqHistory.setStatus("2");
                    int i = activemqHistoryMapper.updateByPrimaryKeySelective(activemqHistory);

                    System.out.println(jmsMessageID + "===success");
                    //成功以后修改状态
               //    countDownLatch.countDown();
                }

                @Override
                public void onException(JMSException e) {

                }
            });
            //阻塞主线程
            acProducer.close();
            //在生产者关闭前添加提交
            // session.commit();
            System.out.println("生产完成!");

        } catch (Exception e) {
            //消息队列回滚
            //  session.rollback();
            throw new Exception(""+e);
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

    public Integer test2(Integer count) {
        counts = count;
        return counts;
    }
}
