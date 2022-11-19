import React from "react";

type FetchState = {
  isLoading: boolean;
  identifier: string | null;
  error: string | null;
  data: null | any;
  extra: null | any;
};

type FetchAction =
  | { type: "SEND"; identifier: string }
  | { type: "RESPONSE"; responseData: any; extra: any }
  | {
      type: "ERROR";
      errorMessage: string;
    }
  | {
      type: "CLEAR";
    };

const initialState: FetchState = {
  isLoading: false,
  identifier: null,
  error: null,
  data: null,
  extra: null,
};

const fetchReducer: React.Reducer<FetchState, FetchAction> = (
  curFetchState,
  action
) => {
  switch (action.type) {
    case "SEND": {
      return {
        isLoading: true,
        error: null,
        data: null,
        extra: null,
        identifier: action.identifier,
      };
    }
    case "RESPONSE": {
      return {
        ...curFetchState,
        isLoading: false,
        data: action.responseData,
        extra: action.extra,
      };
    }
    case "ERROR": {
      return {
        ...curFetchState,
        isLoading: false,
        error: action.errorMessage,
      };
    }
    case "CLEAR": {
      return initialState;
    }
    default: {
      throw new Error("Should not be reached!");
    }
  }
};

const useFetch = () => {
  const [fetchState, dispatchFetch] = React.useReducer<
    React.Reducer<FetchState, FetchAction>
  >(fetchReducer, initialState);

  const clear = React.useCallback(() => {
    dispatchFetch({ type: "CLEAR" });
  }, []);

  const sendRequest = React.useCallback(
    (
      url: string,
      method: string = "GET",
      body: string | null = null,
      reqExtra: any | null = null
    ) => {
      let requestInit: RequestInit = { method: method };
      if (body !== null) {
        requestInit = {
          ...requestInit,
          body: body,
          headers: { "Content-Type": "application/json" },
        };
      }
      dispatchFetch({ type: "SEND", identifier: method });
      fetch(url, requestInit)
        .then((response) => {
          return response.json();
        })
        .then((data) => {
          dispatchFetch({
            type: "RESPONSE",
            responseData: data,
            extra: reqExtra,
          });
        })
        .catch((error) => {
          dispatchFetch({
            type: "ERROR",
            errorMessage: error.message,
          });
        });
    },
    []
  );

  return [fetchState, sendRequest, clear];
};

export default useFetch;
