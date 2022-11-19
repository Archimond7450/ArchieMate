import { render, screen } from "@testing-library/react";
import "@testing-library/jest-dom/extend-expect";
import HomePage from "../pages";

describe("Home Page", () => {
  test("Contains the correct heading", async () => {
    render(<HomePage />);

    const headingElement = await screen.findByRole("heading", {
      name: "The home page",
    });
    expect(headingElement).toBeInTheDocument();
  });

  test("Contains the test button", async () => {
    render(<HomePage />);

    const testButtonElement = await screen.findByRole("button", {
      name: "Test",
    });
    expect(testButtonElement).toBeInTheDocument();
  });
});
