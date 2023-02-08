import React from "react";
import { Box } from "@mui/material";
import almeng from "assets/almeng.png";

/** @jsxImportSource @emotion/react */
import { css } from "@emotion/react";

function ProfileImg({ imgSrc, nickname, width }) {
  const image = () => {
    if (imgSrc) {
      // 프로필 img 있는 경우
      //  들어오는거 보고 고쳐야함
      return <img />;
    } else {
      // 프로필 img 없는 경우
      return (
        <img
          src={almeng}
          alt={nickname}
          css={css`
            width: 100%;
            margin-top: 10%;
          `}
        />
      );
    }
  };

  return (
    <Box
      sx={{
        height: width,
        width: width,
        borderRadius: "200px",
        border: "2px solid #111",
        backgroundColor: "white",
      }}
    >
      {image()}
    </Box>
  );
}

export default ProfileImg;