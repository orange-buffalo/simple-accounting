let lastView: string | null | undefined | symbol;

export function useLastView() {
  const setLastView = (value: string | null | undefined | symbol) => {
    lastView = value;
  };
  return {
    lastView,
    setLastView,
  };
}
