import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// Without this, the screen rendered by one test would still be on the page during the next one.
afterEach(() => cleanup());
