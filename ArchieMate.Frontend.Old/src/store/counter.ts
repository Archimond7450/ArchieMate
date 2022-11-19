import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface ICounterState {
  counter: number;
  visible: boolean;
}

const initialCounterState: ICounterState = { counter: 0, visible: true };

const counterSlice = createSlice({
  name: "counter",
  initialState: initialCounterState,
  reducers: {
    increment(state: ICounterState) {
      state.counter++;
    },
    decrement(state: ICounterState) {
      state.counter--;
    },
    addition(state: ICounterState, action: PayloadAction<number>) {
      state.counter += action.payload;
    },
    clear(state: ICounterState) {
      state.counter = 0;
    },
    toggle(state: ICounterState) {
      state.visible = !state.visible;
    },
  },
});

export const counterReducerActions = counterSlice.actions;

export default counterSlice.reducer;
