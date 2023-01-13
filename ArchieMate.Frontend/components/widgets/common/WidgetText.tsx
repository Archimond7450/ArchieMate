import React from "react";
import WidgetStyles from "../../../styles/widgets/WidgetStyles.module.scss";

const WidgetText = (props: React.PropsWithChildren) => {
  return <div id={WidgetStyles.alertText}>{props.children}</div>;
};

export default WidgetText;
