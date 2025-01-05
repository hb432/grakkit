package grakkit.kontext.api;

public class KMessage {

    /** The message topic. */
    public final String topic;

    /** The message payload. */
    public final String payload;

    /**
     * Constructs a new message with the given topic and payload.
     *
     * @param topic   the message topic
     * @param payload the message payload
     */
    public KMessage(String topic, String payload) {
        this.topic = topic;
        this.payload = payload;
    }
}