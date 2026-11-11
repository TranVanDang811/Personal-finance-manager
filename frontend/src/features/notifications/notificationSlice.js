import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { notificationApi } from '../../services/notificationApi';

export const fetchUnreadCount = createAsyncThunk(
  'notifications/fetchUnreadCount',
  async (_, { rejectWithValue }) => {
    try {
      const res = await notificationApi.getUnreadCount();
      return res.data?.unreadCount ?? 0;
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

export const fetchRecentNotifications = createAsyncThunk(
  'notifications/fetchRecent',
  async (_, { rejectWithValue }) => {
    try {
      const res = await notificationApi.getNotifications({ page: 0, size: 8 });
      return res.data?.content || [];
    } catch (err) {
      return rejectWithValue(err.message);
    }
  }
);

const notificationSlice = createSlice({
  name: 'notifications',
  initialState: {
    unreadCount: 0,
    recent: [],
  },
  reducers: {
    resetNotifications(state) {
      state.unreadCount = 0;
      state.recent = [];
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(fetchUnreadCount.fulfilled, (state, action) => {
        state.unreadCount = action.payload;
      })
      .addCase(fetchRecentNotifications.fulfilled, (state, action) => {
        state.recent = action.payload;
      });
  },
});

export const { resetNotifications } = notificationSlice.actions;
export default notificationSlice.reducer;

