package com.tripplanner.dto;

import com.tripplanner.entity.Activity;
import com.tripplanner.entity.DayPlan;
import java.util.List;
import java.util.stream.Collectors;

public class DayPlanDto {
    private Long id;
    private int dayNumber;
    private List<ActivityDto> activities;
    private Boolean canFitAnotherActivityInTheSameDay;

    public DayPlanDto(Long id, int dayNumber, List<ActivityDto> activities, Boolean canFitAnotherActivityInTheSameDay) {
        this.id = id;
        this.dayNumber = dayNumber;
        this.activities = activities;
        this.canFitAnotherActivityInTheSameDay = canFitAnotherActivityInTheSameDay;
    }

    public static DayPlanDto fromEntity(DayPlan dayPlan) {
        return new DayPlanDto(
                dayPlan.getId(),
                dayPlan.getDayNumber(),
                dayPlan.getActivities().stream().map(ActivityDto::fromEntity).collect(Collectors.toList()),
                dayPlan.getActivities().stream().mapToDouble(Activity::getExpectedDurationHours).sum() < 10
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public List<ActivityDto> getActivities() {
        return activities;
    }

    public void setActivities(List<ActivityDto> activities) {
        this.activities = activities;
    }

    public Boolean getCanFitAnotherActivityInTheSameDay() {

        return canFitAnotherActivityInTheSameDay;
    }

    public void setCanFitAnotherActivityInTheSameDay(Boolean canFitAnotherActivityInTheSameDay) {

        this.canFitAnotherActivityInTheSameDay = canFitAnotherActivityInTheSameDay;
    }
}
