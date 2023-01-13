import React from "react";
import WidgetStyles from "../../../styles/widgets/WidgetStyles.module.scss";

const WidgetHeader = (props: React.PropsWithChildren) => {
  return <div id={WidgetStyles.alertHeader}>{props.children}</div>;
};

export default WidgetHeader;
