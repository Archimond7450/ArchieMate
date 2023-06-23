import React from "react";
import { useRouter } from "next/router";
import channelMessageRepository, {
  ChannelMessage,
} from "../../repositories/ChannelMessageRepository";
import channelRepository from "../../repositories/ChannelRepository";
import WidgetTextWrapper from "./common/WidgetTextWrapper";
import HighlightWrapper from "./common/HighlightWrapper";

enum TextToSpeechWidgetStatus {
  Initial,
  GettingChannelId,
  GettingLastMessage,
  GettingNewestMessages,
}

const brianBaseURI =
  "https://api.streamelements.com/kappa/v2/speech?voice=Brian&text=";

const makeBrianURI = (channelMessage: ChannelMessage): string => {
  return `${brianBaseURI}${encodeURIComponent(
    `${channelMessage.displayName} said: ${channelMessage.message}`
  )}`;
};

interface TextToSpeechWidgetState {
  status: TextToSpeechWidgetStatus;
  channelId: string | null;
  errorMessage: string;
  lastRetrievedMessage: ChannelMessage | null;
  queue: Array<ChannelMessage>;
  currentURI: string;
  cooldownSeconds: number;
  currentMessageData: ChannelMessage | null;
}

const initialState: TextToSpeechWidgetState = {
  status: TextToSpeechWidgetStatus.Initial,
  channelId: null,
  errorMessage: "",
  lastRetrievedMessage: null,
  queue: [],
  currentURI: "",
  cooldownSeconds: 10,
  currentMessageData: null,
};

const TextToSpeechWidget = () => {
  const router = useRouter();
  const { channelName } = router.query;

  const [state, setState] =
    React.useState<TextToSpeechWidgetState>(initialState);

  const createUpdatedState = React.useCallback(
    (newState: Partial<TextToSpeechWidgetState>): TextToSpeechWidgetState => {
      return { ...state, ...newState };
    },
    [state]
  );

  const {
    status,
    channelId,
    errorMessage,
    lastRetrievedMessage,
    queue,
    currentURI,
    cooldownSeconds,
    currentMessageData,
  } = state;

  const removeOneFromQueue = React.useCallback(() => {
    let newQueue = [...queue];
    newQueue.splice(0, 1);
    return newQueue;
  }, [queue]);

  const afterPlayCooldown = () => {
    console.log("Clearing current audio source");
    setState(createUpdatedState({ currentURI: "", currentMessageData: null }));
  };

  const onPlayEnded = () => {
    console.log("onPlayEnded");
    if (cooldownSeconds > 0) {
      setTimeout(() => {
        afterPlayCooldown();
      }, cooldownSeconds * 1000);
    } else {
      afterPlayCooldown();
    }
  };

  React.useEffect(() => {
    if (status === TextToSpeechWidgetStatus.Initial) {
      const interval = setInterval(() => {
        if (typeof channelName === "string" && channelName.length > 0) {
          console.log("Channel name retrieved, changing status");
          setState(
            createUpdatedState({
              status: TextToSpeechWidgetStatus.GettingChannelId,
              errorMessage: "",
            })
          );
        }
      }, 100);

      const timeout = setTimeout(() => {
        console.log("Invalid channelName in address bar");
        setState(
          createUpdatedState({
            errorMessage: "Invalid channelName",
          })
        );
      }, 2500);

      return () => {
        clearInterval(interval);
        clearTimeout(timeout);
      };
    }
  }, [channelName, createUpdatedState, status]);

  React.useEffect(() => {
    if (status === TextToSpeechWidgetStatus.GettingChannelId) {
      channelRepository
        .getByNameAsync((channelName as string).toLowerCase(), (error) => {
          console.log("Error when getting channel info");
          setState(
            createUpdatedState({
              errorMessage: `Error when getting channel info: ${error}`,
            })
          );
        })
        .then((channel) => {
          if (channel !== null) {
            setState(
              createUpdatedState({
                status: TextToSpeechWidgetStatus.GettingLastMessage,
                channelId: channel.id,
                errorMessage: "",
              })
            );
            console.log(channel);
            console.log("Channel info retrieved, changing status again");
          }
        });
    }
  }, [channelName, createUpdatedState, status]);

  React.useEffect(() => {
    if (status === TextToSpeechWidgetStatus.GettingLastMessage) {
      const interval = setInterval(() => {
        channelMessageRepository
          .getLastChannelMessage(channelId as string, (error) => {
            clearInterval(interval);
            console.log("Error while getting last message");
            setState(
              createUpdatedState({
                errorMessage: `Error while getting last message: ${error}`,
              })
            );
            setTimeout(() => {
              setState(
                createUpdatedState({
                  status: TextToSpeechWidgetStatus.GettingChannelId,
                  channelId: null,
                  errorMessage: "",
                })
              );
            }, 1000);
          })
          .then((channelMessage) => {
            if (channelMessage !== null) {
              clearInterval(interval);
              console.log(channelMessage);
              console.log(
                "Last message retrieved, changing status one last time"
              );
              setState(
                createUpdatedState({
                  status: TextToSpeechWidgetStatus.GettingNewestMessages,
                  lastRetrievedMessage: channelMessage,
                  queue: [],
                  errorMessage: "",
                })
              );
            } else {
              console.log("No message yet");
            }
          });
      }, 1000);
      return () => {
        clearInterval(interval);
      };
    }
  }, [
    channelName,
    channelId,
    createUpdatedState,
    status,
    lastRetrievedMessage,
  ]);

  React.useEffect(() => {
    if (status === TextToSpeechWidgetStatus.GettingNewestMessages) {
      const interval = setInterval(() => {
        channelMessageRepository
          .GetAllChannelMessagesFromCertainMessage(
            channelId as string,
            (lastRetrievedMessage as ChannelMessage).messageId,
            (error) => {
              clearInterval(interval);
              console.log("Error while getting newest messages");
              setState(
                createUpdatedState({
                  errorMessage: `Error while getting newest messages: ${error}`,
                })
              );
              setTimeout(() => {
                setState(
                  createUpdatedState({
                    status: TextToSpeechWidgetStatus.GettingLastMessage,
                    errorMessage: "",
                  })
                );
              }, 1000);
            }
          )
          .then((channelMessages) => {
            if (channelMessages !== null) {
              console.log(channelMessages);
              console.log("Newest messages retrieved");
              const prefix = `@${(channelName as string).toLowerCase()}`;
              const filteredMessages = channelMessages
                .filter((messageData) => {
                  return true;
                  //return messageData.message.toLowerCase().startsWith(prefix);
                })
                .map((messageData) => {
                  /*const newMessageData = {
                    ...messageData,
                    message: messageData.message.substring(prefix.length),
                  };
                  return newMessageData;*/
                  return messageData;
                });
              setState(
                createUpdatedState({
                  queue: [...queue, ...filteredMessages],
                  lastRetrievedMessage:
                    channelMessages![channelMessages.length - 1],
                  errorMessage: "",
                })
              );
            } else {
              console.log("No newer messages");
            }
          });
      }, 1000);
      return () => {
        clearInterval(interval);
      };
    }
  }, [
    channelName,
    channelId,
    createUpdatedState,
    lastRetrievedMessage,
    queue,
    status,
  ]);

  React.useEffect(() => {
    if (
      status === TextToSpeechWidgetStatus.GettingNewestMessages &&
      currentURI === "" &&
      currentMessageData === null
    ) {
      if (queue.length === 0) {
        console.log("Queue is empty");
      } else if (queue.length > 0) {
        console.log("Queue is not empty, changing audio source");
        setState(
          createUpdatedState({
            currentURI: makeBrianURI(queue[0]),
            currentMessageData: queue[0],
            queue: removeOneFromQueue(),
          })
        );
      }
    }
  }, [
    currentURI,
    createUpdatedState,
    queue,
    queue.length,
    status,
    removeOneFromQueue,
    currentMessageData,
  ]);

  return (
    <React.Fragment>
      <audio
        src={currentURI}
        autoPlay
        preload="auto"
        onEnded={() => {
          onPlayEnded();
        }}
      />
      {status === TextToSpeechWidgetStatus.Initial && <p>Loading (0/3)</p>}
      {status === TextToSpeechWidgetStatus.GettingChannelId && (
        <p>Loading (1/3)</p>
      )}
      {status === TextToSpeechWidgetStatus.GettingLastMessage && (
        <p>Loading (2/3)</p>
      )}
      {errorMessage !== "" && <WidgetTextWrapper errorMessage={errorMessage} />}
      {currentMessageData !== null && (
        <React.Fragment>
          <WidgetTextWrapper
            header={
              <React.Fragment>
                <HighlightWrapper text={currentMessageData.displayName} />
                &nbsp;said
              </React.Fragment>
            }
            message={currentMessageData.message}
          />
        </React.Fragment>
      )}
    </React.Fragment>
  );
};

export default TextToSpeechWidget;
