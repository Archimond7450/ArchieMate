import React from "react";
import WidgetHeader from "./WidgetHeader";
import WidgetMessage from "./WidgetMessage";
import WidgetText from "./WidgetText";

type WidgetTextWrapperProps =
  | {
      errorMessage: string;
    }
  | {
      errorMessage?: undefined;
      header: JSX.Element;
      message: string;
    };

const WidgetTextWrapper = (props: WidgetTextWrapperProps) => {
  const { finalHeader, finalMessage } = {
    finalHeader: (
      <WidgetHeader>
        {props.errorMessage === undefined ? props.header : "Error"}
      </WidgetHeader>
    ),
    finalMessage: (
      <WidgetMessage>
        {props.errorMessage === undefined ? props.message : props.errorMessage}
      </WidgetMessage>
    ),
  };

  return (
    <WidgetText>
      {finalHeader}
      {finalMessage}
    </WidgetText>
  );
};

export default WidgetTextWrapper;
