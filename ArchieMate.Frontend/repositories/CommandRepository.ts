import { callErrorCallbackOnErrorAsync } from "./common";

const rootEndpoint = "/api/commands";

export interface Command {
  id: string;
  name: string;
  response: string;
}

class CommandRepository {
  async getCommandsByChannelNameAsync(
    channelName: string,
    onErrorCallback?: (error: string) => void
  ) {
    const response = await fetch(`${rootEndpoint}/channel/${channelName}`);
    if (response.ok) {
      return (await response.json()) as Command[];
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }
}

var commandRepository = new CommandRepository();
export default commandRepository;
