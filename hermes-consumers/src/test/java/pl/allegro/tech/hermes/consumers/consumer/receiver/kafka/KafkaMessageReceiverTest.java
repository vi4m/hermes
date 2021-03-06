package pl.allegro.tech.hermes.consumers.consumer.receiver.kafka;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerTimeoutException;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import org.boon.json.JsonFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import pl.allegro.tech.hermes.common.config.ConfigFactory;
import pl.allegro.tech.hermes.common.exception.InternalProcessingException;
import pl.allegro.tech.hermes.common.json.MessageContentWrapper;
import pl.allegro.tech.hermes.consumers.consumer.message.MessageStatus;
import pl.allegro.tech.hermes.consumers.consumer.message.RawMessage;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KafkaMessageReceiverTest {

    private static final String CONTENT_ROOT = "message";
    private static final String TOPIC_NAME = "topic1";
    private static final String CONTENT = "{\"test\":\"a\"}";
    private static final String WRAPPED_MESSAGE_CONTENT = format("{\"_w\":true,\"%s\":%s}", "message", CONTENT);

    @Mock
    private ConsumerConfig consumerConfig;

    @Mock(answer = Answers.RETURNS_MOCKS)
    private ConsumerConnector consumerConnector;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private KafkaStream<byte[], byte[]> kafkaStream;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        Map<String, List<kafka.consumer.KafkaStream<byte[], byte[]>>> consumerMap = Maps.newHashMap();
        consumerMap.put(TOPIC_NAME, ImmutableList.of(kafkaStream).asList());
        when(consumerConnector.createMessageStreams(any(Map.class))).thenReturn(consumerMap);
    }

    @Test
    public void shouldReadMessage() {
        when(kafkaStream.iterator().next().message()).thenReturn(WRAPPED_MESSAGE_CONTENT.getBytes());
        KafkaMessageReceiver kafkaMsgReceiver = getKafkaMessageReceiver();

        RawMessage msg = kafkaMsgReceiver.next();

        assertThat(msg.getStatus()).isEqualTo(MessageStatus.OK);
        assertThat(new String(msg.getData())).isEqualTo(CONTENT);
    }

    @Test
    public void shouldReadEmptyMessageForTimeout() {
        when(kafkaStream.iterator().next()).thenThrow(new ConsumerTimeoutException());
        KafkaMessageReceiver kafkaMsgReceiver = getKafkaMessageReceiver();

        RawMessage msg = kafkaMsgReceiver.next();

        assertThat(msg.getStatus()).isEqualTo(MessageStatus.EMPTY);
        assertThat(msg.getData()).isEqualTo(null);
    }

    @Test(expected = InternalProcessingException.class)
    public void shouldRethrowIfOtherError() {
        when(kafkaStream.iterator().next()).thenThrow(new NullPointerException());
        KafkaMessageReceiver kafkaMsgReceiver = getKafkaMessageReceiver();

        kafkaMsgReceiver.next();
    }



    private KafkaMessageReceiver getKafkaMessageReceiver() {
        return new KafkaMessageReceiver(TOPIC_NAME, consumerConnector, new ConfigFactory(),
                new MessageContentWrapper(CONTENT_ROOT, "metadata", JsonFactory.create()));
    }

}
