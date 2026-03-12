import { Button, Box } from "@mui/material";
import { useContext, useEffect, useState } from "react";
import { AuthContext } from "react-oauth2-code-pkce";
import { useDispatch } from "react-redux";
import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import { setCredentials } from "./store/authSlice";
import ActivityForm from "./components/ActivityForm";
import ActivityList from "./components/ActivityList";
import ActivityDetail from "./components/ActivityDetail";


const ActivityPage = () => {

  const [refresh, setRefresh] = useState(false);

  return (
    <Box component="section" sx={{ p: 2, border: "1px dashed grey" }}>
      <ActivityForm onActivitiesAdded={() => setRefresh(!refresh)} />
      <ActivityList refresh={refresh} />
    </Box>
  );
};
function App() {

  const { token, tokenData, logIn } = useContext(AuthContext);
  const [authReady, setAuthReady] = useState(false);
  const dispatch = useDispatch();

  useEffect(() => {
    if (token) {
      dispatch(setCredentials({ token, user: tokenData }));
      setAuthReady(true);
    }
  }, [token, tokenData, dispatch]);

  return (
    <Router>

      {!token ? (
        <Button
          variant="contained"
          color="primary"
          onClick={() => logIn()}
        >
          LOGIN
        </Button>
      ) : (
        <Box component="section" sx={{ p: 2, border: "1px dashed grey" }}>

          <Routes>
            <Route path="/activities" element={<ActivityPage />} />
            <Route path="/activities/:id" element={<ActivityDetail />} />

            <Route
              path="/"
              element={
                token
                  ? <Navigate to="/activities" replace />
                  : <div>Welcome! Please login.</div>
              }
            />
          </Routes>

        </Box>
      )}

    </Router>
  );
}

export default App;