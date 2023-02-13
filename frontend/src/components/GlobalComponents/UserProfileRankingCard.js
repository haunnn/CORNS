import React from "react";

import sungsil_crown from "assets/sungsil_crown.png";
import ddabong_crown from "assets/ddabong_crown.png";
import suda_crown from "assets/suda_crown.png";
import ingi_crown from "assets/ingi_crown.png";
import { Box, Grid, Typography } from "@mui/material";

/** @jsxImportSource @emotion/react */
import { css } from "@emotion/react";

function UserProfileRankingCard({ rank }) {
  const { ranking, rankCd, value } = rank;

  let crown = "";
  let unit = "";
  let transKorean = "";

  if (rankCd === 2000) {
    crown = sungsil_crown;
    unit = "exp";
    transKorean = "성실";
  } else if (rankCd === 2002) {
    crown = ddabong_crown;
    unit = "개";
    transKorean = "따봉";
  } else if (rankCd === 2001) {
    crown = suda_crown;
    unit = "분";
    transKorean = "수다";
  } else {
    crown = ingi_crown;
    unit = "개";
    transKorean = "인기";
  }

  return (
    <Grid item xs={3}>
      <Box sx={{ border: "3px solid #111", mr: "1.5rem" }}>
        <Box
          sx={{
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            mt: "1rem",
          }}
        >
          <img src={crown} alt={rankCd} />
          <Typography>{transKorean}왕</Typography>
        </Box>
        {(ranking > 0 && ranking < 101) ? (
          <p
            css={css`
              text-align: center;
            `}
          >
            {ranking} 위
          </p>
        ) : (
          <p
            css={css`
              text-align: center;
            `}
          >
            랭크되지 못했어요
          </p>
        )}

        <p
          css={css`
            text-align: center;
          `}
        >
          {value} {unit}
        </p>
      </Box>
    </Grid>
  );
}

export default UserProfileRankingCard;
