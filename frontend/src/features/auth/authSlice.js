import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';
import { authApi } from '../../services/authApi';
import { userApi } from '../../services/userApi';
import { storage } from '../../utils/storage';
import { extractErrorMessage } from '../../utils/formatters';

export const loginUser = createAsyncThunk(
  'auth/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const res = await authApi.login(credentials);
      storage.setAuthSession(res.data);
      return res;
    } catch (err) {
      return rejectWithValue(extractErrorMessage(err, 'Đăng nhập thất bại'));
    }
  }
);

export const registerUser = createAsyncThunk(
  'auth/register',
  async (payload, { rejectWithValue }) => {
    try {
      const res = await authApi.register(payload);
      storage.setAuthSession(res.data);
      return res;
    } catch (err) {
      return rejectWithValue(extractErrorMessage(err, 'Đăng ký tài khoản thất bại'));
    }
  }
);

export const fetchCurrentUser = createAsyncThunk(
  'auth/fetchCurrentUser',
  async (_, { rejectWithValue }) => {
    try {
      const res = await userApi.getCurrentProfile();
      storage.setUser(res.data);
      return res.data;
    } catch (err) {
      return rejectWithValue(extractErrorMessage(err, 'Không thể tải hồ sơ người dùng'));
    }
  }
);

export const logoutUser = createAsyncThunk(
  'auth/logout',
  async (_, { getState }) => {
    const refreshToken = getState().auth.refreshToken || storage.getRefreshToken();
    try {
      await authApi.logout(refreshToken);
    } catch {
      // Clear local session even if token already expired on server
    } finally {
      storage.clearAuthSession();
    }
  }
);

const initialState = {
  accessToken: storage.getAccessToken(),
  refreshToken: storage.getRefreshToken(),
  user: storage.getUser(),
  isAuthenticated: Boolean(storage.getAccessToken()),
  loading: false,
  error: null,
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials(state, action) {
      const { accessToken, refreshToken, user } = action.payload || {};
      state.accessToken = accessToken || state.accessToken;
      state.refreshToken = refreshToken || state.refreshToken;
      if (user) {
        state.user = user;
      }
      state.isAuthenticated = Boolean(state.accessToken);
      state.error = null;
    },
    setUserProfile(state, action) {
      state.user = action.payload;
      storage.setUser(action.payload);
    },
    clearCredentials(state) {
      state.accessToken = null;
      state.refreshToken = null;
      state.user = null;
      state.isAuthenticated = false;
      state.loading = false;
      state.error = null;
      storage.clearAuthSession();
    },
    clearAuthError(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loginUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(loginUser.fulfilled, (state, action) => {
        const data = action.payload.data;
        state.loading = false;
        state.accessToken = data.accessToken;
        state.refreshToken = data.refreshToken;
        state.user = data.user;
        state.isAuthenticated = true;
      })
      .addCase(loginUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(registerUser.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(registerUser.fulfilled, (state, action) => {
        const data = action.payload.data;
        state.loading = false;
        state.accessToken = data.accessToken;
        state.refreshToken = data.refreshToken;
        state.user = data.user;
        state.isAuthenticated = true;
      })
      .addCase(registerUser.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      })
      .addCase(fetchCurrentUser.fulfilled, (state, action) => {
        state.user = action.payload;
      })
      .addCase(logoutUser.fulfilled, (state) => {
        state.accessToken = null;
        state.refreshToken = null;
        state.user = null;
        state.isAuthenticated = false;
        state.loading = false;
        state.error = null;
      });
  },
});

export const { setCredentials, setUserProfile, clearCredentials, clearAuthError } = authSlice.actions;
export default authSlice.reducer;

