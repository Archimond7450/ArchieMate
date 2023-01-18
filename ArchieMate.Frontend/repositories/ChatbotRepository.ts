import { ErrorCallback, callErrorCallbackOnErrorAsync } from "./common";

const rootEndpoint = "/api/chatbot";

function constructEndpoint(endpoint: string = "") {
  return endpoint.length > 0 ? `${rootEndpoint}/${endpoint}` : rootEndpoint;
}

class ChatbotRepository {
  async join(channelName: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(
      constructEndpoint(`join?channelName=${channelName}`)
    );
    if (response.ok) {
      return true;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async part(channelName: string, onErrorCallback?: ErrorCallback) {
    const response = await fetch(
      constructEndpoint(`part?channelName=${channelName}`)
    );
    if (response.ok) {
      return true;
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }

  async status(onErrorCallback?: ErrorCallback) {
    const response = await fetch(constructEndpoint("status"));
    if (response.ok) {
      return (await response.json()) as string[];
    }

    callErrorCallbackOnErrorAsync(response, onErrorCallback);

    return null;
  }
}

var chatbotRepository = new ChatbotRepository();
export default chatbotRepository;
