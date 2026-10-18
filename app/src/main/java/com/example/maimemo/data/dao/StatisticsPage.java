package com.example.maimemo.data.dao;

import java.util.List;

public class StatisticsPage {
    //总体思想：每背一个单词就上传到C#端计算并且返回数据

    //遗忘曲线部分
    int SixHour;
    int OneDay;
    int TwoDay;
    int ThreeDay;
    int SixDay;
    int SevenDay;
    int MemoryRate;
    float AverageReviewGapDay;
    int TotallyReviewCount;
    int TwentyRate;
    int OneHourRate;
    int OneDayRate;
    String[] RemindWord;



    //学习情况部分
    //FROM HOMEPAGE
    int[] CurrentDayCount = new int[7];
    int NewLearn;
    int Review;
    int Accuracy;
    int WeekCount;
    int Morning;
    int Noon;
    int Afternoon;
    //FROM MINE
    List<BookSituation> StudyDegreen;

    //记忆持久度部分
    int AverageMemoryRate;
    int LongTimeRate;
    int MediumTimeRate;
    int ShortTimeRate;
    int[] EfficientWord = new int[7];
    String[] EasyToForget;
    String Suggestion;

    StatisticsPage(){

    }
}
