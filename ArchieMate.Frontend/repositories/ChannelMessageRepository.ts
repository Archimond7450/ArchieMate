import { ErrorCallback, callErrorCallbackOnErrorAsync } from "./common";

const rootEndpoint = "/api/channelmessage";

export interface ChannelMessage {
  messageId: string;
  displayName: string;
  message: string;
}

function constructEndpoint(endpoint: string = "") {
  return endpoint.length > 0 ? `${rootEndpoint}/${endpoint}` : rootEndpoint;
}

class ChannelMessageRepository {
  async getLastChannelMessage(
    channelId: string,
    onErrorCallback?: ErrorCallback
  ) {
    const response = await fetch(constructEndpoint(`${channelId}/last`));
    if (response.ok) {
      if (response.status === 200) {
        return (await response.json()) as ChannelMessage;
      }
      if (response.status === 204) {
        return null;
      }
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async GetAllChannelMessagesFromCertainMessage(
    channelId: string,
    messageFromId: string,
    onErrorCallback?: ErrorCallback
  ) {
    const response = await fetch(
      constructEndpoint(`${channelId}/from/${messageFromId}`)
    );
    if (response.ok) {
      if (response.status === 200) {
        return (await response.json()) as Array<ChannelMessage>;
      }
      if (response.status === 204) {
        return null;
      }
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async GetAll(onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint());
    if (response.ok) {
      return (await response.json()) as Array<ChannelMessage>;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }
}

var channelMessageRepository = new ChannelMessageRepository();
export default channelMessageRepository;
