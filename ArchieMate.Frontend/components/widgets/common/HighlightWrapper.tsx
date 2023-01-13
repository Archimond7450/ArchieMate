import React from "react";
import WidgetStyles from "../../../styles/widgets/WidgetStyles.module.scss";
import Animations from "../../../styles/widgets/Animations.module.scss";

interface HighlightWrapperProps {
  text: string;
}

const HighlightWrapper = (props: HighlightWrapperProps) => {
  const highlightLetters = (letter: string) => {
    return (
      <span className={`${Animations.animatedLetter} ${Animations.wiggle}`}>
        {letter}
      </span>
    );
  };

  const { text } = props;
  const textAsArray = [...text];
  const highlightedLetters = textAsArray.map(highlightLetters);
  return (
    <span className={WidgetStyles.highlightWrapper}>{highlightedLetters}</span>
  );
};

export default HighlightWrapper;
