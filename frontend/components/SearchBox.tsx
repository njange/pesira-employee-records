import { useEffect, useState } from "react";
import TextField from "./TextField";

const DEBOUNCE_MS = 300;

interface SearchBoxProps {
  value: string;
  onSearch: (value: string) => void;
}

export default function SearchBox({ value, onSearch }: SearchBoxProps) {
  const [text, setText] = useState(value);
  const [lastValue, setLastValue] = useState(value);

  // The URL changed from outside (Back button, Clear search): show its value.
  if (value !== lastValue) {
    setLastValue(value);
    setText(value);
  }

  useEffect(() => {
    if (text === value) return;
    const timer = setTimeout(() => onSearch(text), DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [text, value, onSearch]);

  return (
    <TextField
      id="search"
      label="Search"
      type="search"
      placeholder="Name, email or job title"
      value={text}
      onChange={(event) => setText(event.target.value)}
    />
  );
}
