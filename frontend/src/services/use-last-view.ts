let lastView: string | null | undefined;

export function useLastView() {
  const setLastView = (value: string | null | undefined) => {
    lastView = value;
  };
  return {
    lastView,
    setLastView,
  };
}
