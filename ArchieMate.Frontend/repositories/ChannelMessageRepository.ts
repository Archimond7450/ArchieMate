import { callErrorCallbackOnErrorAsync } from "./common";

const rootEndpoint = "/api/channelmessage";

export interface ChannelMessage {
  messageId: string;
  displayName: string;
  message: string;
}

class ChannelMessageRepository {
  async getLastChannelMessage(
    channelId: string,
    onErrorCallback?: (error: string) => void
  ): Promise<ChannelMessage | null> {
    const response = await fetch(`${rootEndpoint}/${channelId}/last`);
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
    onErrorCallback?: (error: string) => void
  ): Promise<Array<ChannelMessage> | null> {
    const response = await fetch(
      `${rootEndpoint}/${channelId}/from/${messageFromId}`
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
}

var channelMessageRepository: ChannelMessageRepository =
  new ChannelMessageRepository();
export default channelMessageRepository;
