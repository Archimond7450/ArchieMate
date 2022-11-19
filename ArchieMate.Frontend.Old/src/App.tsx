import React, { Fragment } from "react";
import { Route } from "react-router-dom";

import LandingPage from "./pages/Landing";
import DashboardPage from "./pages/Dashboard";

const App = () => {
  return (
    <Fragment>
      <Route>
        <LandingPage />
      </Route>
      <Route path="/dashboard">
        <DashboardPage />
      </Route>
    </Fragment>
  );
};

export default App;
