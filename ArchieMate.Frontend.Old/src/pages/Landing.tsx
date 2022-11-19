import React from "react";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import TextField from "@mui/material/TextField";
import { useSelector, useDispatch } from "react-redux";
import { RootState } from "../store";
import { counterReducerActions } from "../store/counter";

const LandingPage = () => {
  const dispatch = useDispatch();
  const counter = useSelector((state: RootState) => state.counter.counter);
  const visible = useSelector((state: RootState) => state.counter.visible);
  const amountRef = React.useRef<HTMLInputElement>();

  const incrementHandler = () => {
    dispatch(counterReducerActions.increment());
  };

  const decrementHandler = () => {
    dispatch(counterReducerActions.decrement());
  };

  const additionHandler = () => {
    const currentRef = amountRef.current;
    if (currentRef) {
      dispatch(counterReducerActions.addition(+currentRef.value));
    }
  };

  const clearHandler = () => {
    dispatch(counterReducerActions.clear());
  };

  const toggleCounterHandler = () => {
    dispatch(counterReducerActions.toggle());
  };

  return (
    <Box
      sx={{ backgroundColor: "silver" }}
      component="form"
      noValidate
      autoComplete="off"
    >
      <Button onClick={incrementHandler} variant="contained">
        +
      </Button>
      <Button onClick={decrementHandler} variant="contained">
        -
      </Button>
      <Button onClick={clearHandler} variant="contained">
        0
      </Button>
      <Button onClick={additionHandler} variant="contained">
        +x
      </Button>
      <TextField inputRef={amountRef} label="Amount" type="number" />
      <Button onClick={toggleCounterHandler} variant="contained">
        Toggle
      </Button>
      {visible && <div>{counter}</div>}
    </Box>
  );
};

export default LandingPage;
