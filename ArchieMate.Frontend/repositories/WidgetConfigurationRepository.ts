import {
  CreateDTO,
  ErrorCallback,
  callErrorCallbackOnErrorAsync,
} from "./common";

const rootEndpoint = "/api/configuration/widgets";
const ttsRootEndpoint = `${rootEndpoint}/tts`;

export type TTSWidgetConfiguration = {
  enabled: boolean;
  onlyFromMentions: boolean;
  delayBetweenMessages: boolean;
  volumePercent: number;
  maxDuration: number;
  allowEmoteOnly: boolean;
  censoring: boolean;
};

export type WidgetsConfiguration = {
  textToSpeech: TTSWidgetConfiguration;
};

function constructTTSEndpoint(channelId: string) {
  return `${ttsRootEndpoint}/${channelId}`;
}

function constructEndpoint(endpoint: string = "") {
  return endpoint.length > 0 ? `${rootEndpoint}/${endpoint}` : rootEndpoint;
}

class TTSWidgetConfigurationRepository {
  async update(
    channelId: string,
    configuration: TTSWidgetConfiguration,
    onErrorCallback?: ErrorCallback
  ) {
    const response = await fetch(constructTTSEndpoint(channelId), {
      method: "PUT",
    });
    if (response.ok) {
      return true;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async reset(channelId: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructTTSEndpoint(channelId), {
      method: "DELETE",
    });
    if (response.ok) {
      return true;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }
}

class WidgetConfigurationRepository {
  async getByChannelId(channelId: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint(channelId));
    if (response.ok) {
      return (await response.json()) as WidgetsConfiguration;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async reset(channelId: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint(channelId), {
      method: "DELETE",
    });
    if (response.ok) {
      return true;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  TextToSpeech = new TTSWidgetConfigurationRepository();
}

var widgetConfigurationRepository = new WidgetConfigurationRepository();
export default widgetConfigurationRepository;
