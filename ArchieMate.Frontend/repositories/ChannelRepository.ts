import {
  CreateDTO,
  ErrorCallback,
  callErrorCallbackOnErrorAsync,
} from "./common";

const rootEndpoint = "/api/channel";

export type Channel = {
  id: string;
  name: string;
  roomId: number;
  join: boolean;
};

export type CreateChannelDTO = CreateDTO<Channel>;

function constructEndpoint(endpoint: string = "") {
  return endpoint.length > 0 ? `${rootEndpoint}/${endpoint}` : rootEndpoint;
}

class ChannelRepository {
  async getAllAsync(onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint());
    if (response.ok) {
      return (await response.json()) as Channel[];
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async getAsync(id: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint(id));
    if (response.ok) {
      return (await response.json()) as Channel;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async getByRoomIdAsync(id: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint(`id/${id}`));
    if (response.ok) {
      return (await response.json()) as Channel;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async getByNameAsync(name: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint(`name/${name}`));
    if (response.ok) {
      if (response.status === 200) {
        return (await response.json()) as Channel;
      }
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async addAsync(
    newChannel: CreateChannelDTO,
    onErrorCallback?: ErrorCallback
  ) {
    const response = await fetch(constructEndpoint(), {
      method: "POST",
      body: JSON.stringify(newChannel),
    });
    if (response.ok) {
      return (await response.json()) as Channel;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async updateAsync(channel: Channel, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint(channel.id), {
      method: "PUT",
      body: JSON.stringify(channel),
    });
    if (response.ok) {
      return;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async deleteAsync(id: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint(id), {
      method: "DELETE",
    });
    if (response.ok) {
      return;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }
}

var channelRepository = new ChannelRepository();
export default channelRepository;
