import { EventObject, EventType } from "../types";
import _ from "lodash";
import { Config } from "../config/Config";

export class Event {
  private readonly _state: boolean;
  private readonly _time: Temporal.ZonedDateTime;
  private readonly _type: EventType;

  constructor(state: boolean, type: EventType, time: Temporal.ZonedDateTime) {
    this._state = state;
    this._time = time;
    this._type = type;
  }

  get state(): boolean {
    return this._state;
  }

  get time(): Temporal.ZonedDateTime {
    return this._time;
  }

  get type() {
    return this._type;
  }

  get obj(): EventObject {
    return {
      type: this.type,
      state: this.state,
      time: this.time.toString(),
    };
  }

  previousOfSameType(
    config: Config,
    predicate: (e: Event) => boolean = () => true,
  ): Event | undefined {
    return config.previousEvent(this.type, predicate);
  }

  since(earlier: Event) {
    return this.time.toInstant().since(earlier.time.toInstant());
  }

  static fromObj(obj: EventObject): Event {
    return new Event(obj.state, obj.type, Temporal.ZonedDateTime.from(obj.time));
  }
}
