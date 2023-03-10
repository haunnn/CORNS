package com.w6w.corns.service.growth;

import com.w6w.corns.domain.cal.CalLog;
import com.w6w.corns.domain.cal.CalLogRepository;
import com.w6w.corns.domain.explog.ExpLog;
import com.w6w.corns.domain.explog.ExpLogRepository;
import com.w6w.corns.domain.loginlog.LoginLogRepository;
import com.w6w.corns.domain.room.RoomRepository;
import com.w6w.corns.domain.roomuser.RoomUser;
import com.w6w.corns.domain.roomuser.RoomUserRepository;
import com.w6w.corns.domain.user.User;
import com.w6w.corns.domain.user.UserRepository;
import com.w6w.corns.dto.explog.ExpLogRequestDto;
import com.w6w.corns.dto.explog.ExpLogResponseDto;
import com.w6w.corns.dto.indicator.IndicatorResponseDto;
import com.w6w.corns.dto.indicator.SubjectRatioResponseDto;
import com.w6w.corns.dto.level.LevelDto;

import com.w6w.corns.service.subject.SubjectService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.IntStream;

import com.w6w.corns.util.PageableResponseDto;
import com.w6w.corns.util.code.RankCode;
import com.w6w.corns.util.code.RoomUserCode;
import com.w6w.corns.util.code.UserCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GrowthServiceImpl implements GrowthService {

    private final UserRepository userRepository;
    private final ExpLogRepository expLogRepository;
    private final LoginLogRepository loginLogRepository;
    private final RoomUserRepository roomUserRepository;
    private final RoomRepository roomRepository;
    private final SubjectService subjectService;
    private final CalLogRepository calLogRepository;

    public int calExpPercentile(int userId) throws Exception{

        //user에서 usercd 탈퇴, 정지를 제외한 나머지 사람들의 총 exp 순위
        int cntAll = userRepository.countByUserCd(UserCode.USER_DEFAULT.getCode());

        int rank =  userRepository.rankByExp(userId);

        double ans = (double)rank / cntAll * 100;
        return (int)ans;
    }

    public Map<String, Object> getUserLevel(int userId) throws Exception{

        Map<String, Object> result = new HashMap<>();

        User user = userRepository.findByUserId(userId);

        result.put("level", LevelDto.fromEntity(user.getLevel()));
        result.put("expTotal", user.getExpTotal());

        return result;
    }

    public PageableResponseDto getExpLogList(int userId, Pageable pageable, String baseTime) throws Exception{
        //baseTime -> LocalDate 타입으로
        LocalDateTime localDateTime = LocalDateTime.parse(baseTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        Slice<ExpLog> slice = expLogRepository.findByUserIdAndRegTmLessThanEqual(userId, localDateTime, pageable);

        List<ExpLogResponseDto> users = new ArrayList<>();
        for(ExpLog expLog : slice.getContent())
            users.add(ExpLogResponseDto.fromEntity(expLog));

        return PageableResponseDto.builder().list(users).hasNext(slice.hasNext()).build();
    }

    //경험치 부여
    @Override
    @Transactional
    public void giveExp(ExpLogRequestDto expLogRequestDto){
        //expTotal 증가
        User user = userRepository.findByUserId(expLogRequestDto.getUserId());
        user.setExpTotal(user.getExpTotal()+expLogRequestDto.getGainExp());
        userRepository.save(user);

        expLogRepository.save(expLogRequestDto.toEntity());
    }

    //출석률 반환
    @Override
    public int calAttendanceRate(int userId) throws Exception {

        long countPerMonth = loginLogRepository.findByRegTmAndUserIdPerMonth(userId);

        double rate = (double)countPerMonth / 30;
        return (int)(rate * 100);
    }

    @Override
    public List<IndicatorResponseDto> calDailySpeakingTotalByWeek(int userId) throws Exception {

        List<IndicatorResponseDto> responseDtos = new ArrayList<>();

        //roomuser에 있는 speaking_sec를 일별로 받아오기 -> 나중에 계산테이블 이용
        for(int i=6; i>=0; i--){
            LocalDate date = LocalDate.now().minusDays(i);

            CalLog calLog = calLogRepository.findByUserIdAndRankCdAndStartDtAndEndDt(userId, RankCode.RANK_SPEAKING.getCode(), date, date);
            responseDtos.add(IndicatorResponseDto.builder()
                    .x(date.toString())
                    .y(calLog==null?"0": String.valueOf(calLog.getValue()/60))
                    .build());
        }

        return responseDtos;
    }

    //주제별 비율 계산
    @Override
    public List<SubjectRatioResponseDto> countBySubject(int userId) throws Exception {

        List<SubjectRatioResponseDto> subjectRatio = new ArrayList<>();

        //roomuser에서 userid로 모든 대화 기록 가져오기
        List<RoomUser> roomUsers = roomUserRepository.findByUserIdAndRoomUserCd(userId, RoomUserCode.ROOM_USER_END.getCode());

        int n = subjectService.findAll().size();
        int[] count = new int[n+1];

        //각 roomuser의 room 번호로 room에서 대화 주제 가져오기
        for(RoomUser roomuser : roomUsers){
            count[roomRepository.findById(roomuser.getRoomNo()).get().getSubjectNo()]++;
        }

        //총 대화 주제 수
        int sum = IntStream.of(count).sum();

        //주제별 비율 리스트
        for(int i=1; i<=n; i++){
            SubjectRatioResponseDto responseDto = SubjectRatioResponseDto.builder()
                    .id(subjectService.findById(i).getValue())
                    .label(subjectService.findById(i).getValue())
                    .value(count[i])
                    .build();
            subjectRatio.add(responseDto);
        }
        return subjectRatio;
    }

    //일일 경험치 획득량 계산
    @Override
    public Map<String, Object> calDailyGainedExp(int userId) throws Exception {
        //만약 사용자가 가입한지 얼마 안됐다면 날짜를 자를건지, 아니면 0으로 보여줄건지

        List<IndicatorResponseDto> lastWeek = new ArrayList<>();
        List<IndicatorResponseDto> thisWeek = new ArrayList<>();

        for(int i=13; i>=0; i--){
            LocalDate date = LocalDate.now().minusDays(i);

            CalLog calLog = calLogRepository.findByUserIdAndRankCdAndStartDtAndEndDt(userId, RankCode.RANK_EXP.getCode(), date, date);
            DayOfWeek dayOfWeek = date.getDayOfWeek();

            IndicatorResponseDto temp = IndicatorResponseDto.builder()
                    .x(dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.KOREAN))
                    .y(calLog==null?"0": String.valueOf(calLog.getValue())).build();
            if(i / 7 > 0) lastWeek.add(temp);
            else thisWeek.add(temp);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("lastWeek", lastWeek);
        result.put("thisWeek", thisWeek);
        return result;
    }
}
