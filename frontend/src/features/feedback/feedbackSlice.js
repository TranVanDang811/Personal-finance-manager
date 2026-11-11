import { createSlice } from '@reduxjs/toolkit';

let nextToastId = 1;

const feedbackSlice = createSlice({
  name: 'feedback',
  initialState: {
    toasts: [],
  },
  reducers: {
    showToast: {
      reducer(state, action) {
        state.toasts.push(action.payload);
      },
      prepare({ type = 'info', message }) {
        return {
          payload: {
            id: nextToastId++,
            type,
            message,
          },
        };
      },
    },
    dismissToast(state, action) {
      state.toasts = state.toasts.filter((t) => t.id !== action.payload);
    },
  },
});

export const { showToast, dismissToast } = feedbackSlice.actions;
export default feedbackSlice.reducer;

