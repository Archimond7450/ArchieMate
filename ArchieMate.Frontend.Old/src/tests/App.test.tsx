import React from "react";
import { render, screen } from "@testing-library/react";
import App from "../App";

describe("ArchieMate Frontend App", () => {
  test("Renders ArchieMate", () => {
    render(<App />);
    const linkElement = screen.getByText(/ArchieMate/i);
    expect(linkElement).toBeInTheDocument();
  });
});
