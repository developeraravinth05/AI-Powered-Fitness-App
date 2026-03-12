import React, { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import { getActivityDetail } from "../services/api";
import { Typography, Card, CardContent, Box } from "@mui/material";

const ActivityDetail = () => {

  const { id } = useParams();
  const [activity, setActivity] = useState(null);
  const [recommendation, setRecommendation] = useState(null);

  useEffect(() => {
    const fetchActivityDetail = async () => {
      try {
        const response = await getActivityDetail(id);
        setActivity(response.data);
        setRecommendation(response.data.recommendation);
      } catch (error) {
        console.error(error);
      }
    };

    fetchActivityDetail();
  }, [id]);

  if (!activity) {
    return <Typography>Loading...</Typography>;
  }

  return (
    <Box sx={{ p: 3 }}>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h5" gutterBottom>
            Activity Detail
          </Typography>

          <Typography>
            Type: {activity.type}
          </Typography>

          <Typography>
            Duration: {activity.duration} minutes
          </Typography>

          <Typography>
            Calories Burned: {activity.caloriesBurned}
          </Typography>

          <Typography>
            Start Time: {activity.startTime}
          </Typography>

        </CardContent>
      </Card>

      {recommendation && (
        <Card>
          <CardContent>
            <Typography variant="h6">
              AI Recommendation
            </Typography>

            <Typography>
              {recommendation}
            </Typography>
          </CardContent>
        </Card>
      )}

    </Box>
  );
};

export default ActivityDetail;