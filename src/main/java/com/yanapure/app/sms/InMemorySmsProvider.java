package com.yanapure.app.sms;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.springframework.stereotype.Component;

/**
 * In-memory SMS provider for testing and development Stores sent messages in memory for
 * verification
 */
@Component
public class InMemorySmsProvider implements SmsProvider {

  private final Map<String, Queue<String>> sentMessages = new ConcurrentHashMap<>();

  @Override
  public boolean sendSms(String phoneNumber, String message) {
    if (phoneNumber == null || message == null) {
      return false;
    }

    sentMessages.computeIfAbsent(phoneNumber, k -> new ConcurrentLinkedQueue<>()).offer(message);

    return true;
  }

  @Override
  public String getProviderName() {
    return "InMemorySmsProvider";
  }

  /**
   * Get the last message sent to a phone number
   *
   * @param phoneNumber E.164 formatted phone number
   * @return last message sent, or null if none
   */
  public String getLastMessage(String phoneNumber) {
    Queue<String> messages = sentMessages.get(phoneNumber);
    if (messages == null || messages.isEmpty()) {
      return null;
    }
    // Convert to array to get the last element
    String[] messageArray = messages.toArray(new String[0]);
    return messageArray[messageArray.length - 1];
  }

  /**
   * Get all messages sent to a phone number
   *
   * @param phoneNumber E.164 formatted phone number
   * @return queue of messages sent
   */
  public Queue<String> getAllMessages(String phoneNumber) {
    return sentMessages.getOrDefault(phoneNumber, new ConcurrentLinkedQueue<>());
  }

  /** Clear all stored messages (useful for testing) */
  public void clearMessages() {
    sentMessages.clear();
  }

  /**
   * Get count of messages sent to a phone number
   *
   * @param phoneNumber E.164 formatted phone number
   * @return number of messages sent
   */
  public int getMessageCount(String phoneNumber) {
    Queue<String> messages = sentMessages.get(phoneNumber);
    return messages != null ? messages.size() : 0;
  }
}
