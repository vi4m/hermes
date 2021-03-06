package pl.allegro.tech.hermes.domain.subscription.offset;

import pl.allegro.tech.hermes.api.TopicName;

public interface SubscriptionOffsetChangeIndicator {

    void setSubscriptionOffset(TopicName topicName, String subscriptionName, String brokersClusterName, int partitionId, Long offset);

    PartitionOffsets getSubscriptionOffsets(TopicName topicName, String subscriptionName, String brokersClusterName);

}
