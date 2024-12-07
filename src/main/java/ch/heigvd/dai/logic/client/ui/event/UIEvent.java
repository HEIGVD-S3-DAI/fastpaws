package ch.heigvd.dai.logic.client.ui.event;

public class UIEvent {
  private final EventType type;
  private final Object data;

  public UIEvent(EventType type, Object data) {
    this.type = type;
    this.data = data;
  }

  public EventType getType() {
    return type;
  }

  public Object getData() {
    return data;
  }

  public enum EventType {
    RACE_TEXT_RECEIVED,
    GAME_STATE_CHANGED,
    PLAYER_PROGRESS_UPDATED
  }
}
