package com.w6w.corns.domain.room;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.w6w.corns.domain.roomuser.QRoomUser;
import com.w6w.corns.util.code.RoomCode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Transactional
public class CustomRoomRepositoryImpl implements CustomRoomRepository {

    private final EntityManager em;
    private final JPAQueryFactory jpaQueryFactory;

    private QRoom room = QRoom.room;
    private QRoomUser roomUser = QRoomUser.roomUser;

    public CustomRoomRepositoryImpl(EntityManager em) {
        this.em = em;
        this.jpaQueryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Slice<Room> searchBySlice(String baseTime, ArrayList<Integer> subjects, int minTime, int maxTime, boolean isAvail, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        // baseTime
        builder.and(room.regTm.lt(LocalDateTime.parse(baseTime, DateTimeFormatter.ofPattern(("yyyy-MM-dd HH:mm:ss")))));

        // subject
        BooleanBuilder subjectBuilder = new BooleanBuilder();
        for (int subject: subjects) {
            subjectBuilder.or(room.subjectNo.eq(subject));
        }
        builder.and(subjectBuilder);

        // time
        builder.and(room.time.between(minTime, maxTime));

        // isAvail
        if (isAvail) {
            builder.and(room.roomCd.eq(RoomCode.ROOM_WAITING.getCode()));
            builder.and(room.maxMember.gt(
                    jpaQueryFactory.select(roomUser.count())
                            .from(roomUser)
                            .where(roomUser.roomNo.eq(room.roomNo))));
        } else {
            builder.and(room.roomCd.loe(RoomCode.ROOM_START.getCode()));
        }

        List<Room> rooms = jpaQueryFactory.selectFrom(room)
                .where(builder)
                .orderBy(room.regTm.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        return checkLastPage(pageable, rooms);
    }

    // ?????? ????????? ?????? ???????????? ?????????
    private Slice<Room> checkLastPage(Pageable pageable, List<Room> results) {

        boolean hasNext = false;

        // ????????? ?????? ????????? ????????? ????????? ??????????????? ?????? ?????? ??? ??????, next = true
        if (results.size() > pageable.getPageSize()) {
            hasNext = true;
            results.remove(pageable.getPageSize());
        }

        return new SliceImpl<>(results, pageable, hasNext);
    }

}
