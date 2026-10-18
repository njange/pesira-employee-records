import type { ButtonHTMLAttributes } from "react";
import styles from "./Button.module.css";

interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, "className"> {
  type: "button" | "submit";
  variant?: "primary" | "secondary" | "danger";
  size?: "normal" | "small";
}

export default function Button({ variant = "secondary", size = "normal", ...props }: ButtonProps) {
  return <button className={`${styles.button} ${styles[variant]} ${styles[size]}`} {...props} />;
}
