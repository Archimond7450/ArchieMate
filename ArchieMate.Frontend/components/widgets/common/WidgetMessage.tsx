import React from "react";
import WidgetStyles from "../../../styles/widgets/WidgetStyles.module.scss";

const WidgetMessage = (props: React.PropsWithChildren) => {
  return <span id={WidgetStyles.alertMessage}>{props.children}</span>;
};

export default WidgetMessage;
