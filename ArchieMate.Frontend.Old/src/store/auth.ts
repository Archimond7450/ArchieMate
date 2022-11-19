import { createSlice } from "@reduxjs/toolkit";

interface IAuthState {
  isAuthenticated: boolean;
}

const initialAuthState: IAuthState = { isAuthenticated: false };

const authSlice = createSlice({
  name: "auth",
  initialState: initialAuthState,
  reducers: {
    login(state: IAuthState) {
      state.isAuthenticated = true;
    },
    logout(state: IAuthState) {
      state.isAuthenticated = false;
    },
  },
});

export const authReducerActions = authSlice.actions;

export default authSlice.reducer;
