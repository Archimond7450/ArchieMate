import React from "react";
import { Card } from "@mui/material";

const Code = (props: React.PropsWithChildren) => {
  return (
    <Card>
      <pre style={{ margin: 5 }}>
        <code>{props.children}</code>
      </pre>
    </Card>
  );
};

export default Code;
