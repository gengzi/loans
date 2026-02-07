//package com.gengzi.pay.flink;
//
//import org.apache.flink.api.common.functions.FlatMapFunction;
//import org.apache.flink.api.java.DataSet;
//import org.apache.flink.api.java.ExecutionEnvironment;
//import org.apache.flink.api.java.tuple.Tuple2;
//import org.apache.flink.configuration.Configuration;
//import org.apache.flink.util.Collector;
//
///**
// * 单词统计（批数据处理）
// */
//public class WordCount {
//    public static void main(String[] args) throws Exception {
//        // 输入路径和出入路径通过参数传入，约定第一个参数为输入路径，第二个参数为输出路径
//        String inPath = "E:\\ruanjian\\01 01 课程资料\\Flink讲义java高薪课\\1.txt";
//        String outPath = "";
////        Configuration conf = new Configuration();
////        conf.setLong("taskmanager.memory.off-heap.size", 0);
//        // 获取Flink批处理执行环境
//        ExecutionEnvironment executionEnvironment = ExecutionEnvironment.getExecutionEnvironment();
//        // 获取文件中内容
//        DataSet<String> text = executionEnvironment.readTextFile(inPath);
//        // 对数据进行处理
//        DataSet<Tuple2<String, Integer>> dataSet = text.flatMap(new LineSplitter()).groupBy(0).sum(1);
////        dataSet.writeAsCsv("E:\\ruanjian\\01 01 课程资料\\Flink讲义java高薪课","\n","").setParallelism(1);
//        dataSet.writeAsText("E:\\ruanjian\\01 01 课程资料\\Flink讲义java高薪课");
//        // 触发执行程序
//        executionEnvironment.execute("wordcount batch process");
//    }
//
//
//    static class LineSplitter implements FlatMapFunction<String, Tuple2<String,Integer>> {
//        @Override
//        public void flatMap(String line, Collector<Tuple2<String, Integer>> collector) throws Exception {
//            for (String word:line.split(" ")) {
//                collector.collect(new Tuple2<>(word,1));
//            }
//        }
//    }
//}