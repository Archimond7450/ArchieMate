const rootEndpoint = "/api/channel";

export interface Channel {
  id: string;
  name: string;
  roomId: number;
  join: boolean;
}

class ChannelRepository {
  async getByNameAsync(
    name: string,
    onErrorCallback?: (error: string) => void
  ): Promise<Channel | null> {
    const response = await fetch(`${rootEndpoint}/name/${name}`);
    if (response.ok) {
      if (response.status === 200) {
        return (await response.json()) as Channel;
      }
    }
    if (onErrorCallback !== undefined) {
      onErrorCallback(
        `${response.status} - ${
          response.statusText
        } => ${await response.text()}`
      );
    }
    return null;
  }
}

var channelRepository: ChannelRepository = new ChannelRepository();
export default channelRepository;
