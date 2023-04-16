/*"jest": {
    "preset": "next/babel",
    "testPathIgnorePatterns": [
      "<rootDir>/.next/",
      "<rootDir>/node_modules/"
    ],
    "transform": {
      "^.+\\.(js|jsx|ts|tsx)$": "<rootDir>/node_modules/babel-jest"
    },
    "moduleNameMapper": {
      "\\.(jpg|jpeg|png|gif|eot|otf|webp|svg|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$": "<rootDir>/__mocks__/fileMock.ts",
      "\\.(css|less)$": "<rootDir>/__mocks__/styleMock.ts"
    },
    "setupFilesAfterEnv": [
      "./setupTests.ts"
    ],
    "testEnvironment": "jsdom",
    "coverageReporters": [
      "cobertura"
    ]
  }*/

import nextJest from "next/jest";
const createJestConfig = nextJest({
  dir: "./",
});
const customJestConfig = {
  moduleDirectories: ["node_modules", "<rootDir>/"],
  testEnvironment: "jest-environment-jsdom",
  coverageReporters: ["cobertura"],
};
export default createJestConfig(customJestConfig);
