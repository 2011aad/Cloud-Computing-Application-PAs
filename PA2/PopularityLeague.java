import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ArrayWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.KeyValueTextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.io.*;
import java.util.*;

public class PopularityLeague extends Configured implements Tool {

    public static String delimiters = " :";

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(new Configuration(), new PopularityLeague(), args);
        System.exit(res);
    }

    public static class IntArrayWritable extends ArrayWritable {
        public IntArrayWritable() {
            super(IntWritable.class);
        }

        public IntArrayWritable(Integer[] numbers) {
            super(IntWritable.class);
            IntWritable[] ints = new IntWritable[numbers.length];
            for (int i = 0; i < numbers.length; i++) {
                ints[i] = new IntWritable(numbers[i]);
            }
            set(ints);
        }
    }

    @Override
    public int run(String[] args) throws Exception {
        // TODO
        Configuration conf = this.getConf();
        FileSystem fs = FileSystem.get(conf);
        Path tmpPath = new Path("/mp2/tmp");
        fs.delete(tmpPath, true);

        Job jobA = Job.getInstance(conf, "Popularity League");
        jobA.setOutputKeyClass(IntWritable.class);
        jobA.setOutputValueClass(IntWritable.class);

        jobA.setMapperClass(LinkCountMap.class);
        jobA.setReducerClass(LinkCountReduce.class);

        FileInputFormat.setInputPaths(jobA, new Path(args[0]));
        FileOutputFormat.setOutputPath(jobA, tmpPath);

        jobA.setJarByClass(PopularityLeague.class);
        jobA.waitForCompletion(true);

        Job jobB = Job.getInstance(conf, "Popularity League");
        jobB.setOutputKeyClass(IntWritable.class);
        jobB.setOutputValueClass(IntWritable.class);

        jobB.setMapOutputKeyClass(IntWritable.class);
        jobB.setMapOutputValueClass(IntWritable.class);

        jobB.setMapperClass(LeagueRankMap.class);
        jobB.setReducerClass(LeagueRankReduce.class);
        jobB.setNumReduceTasks(1);

        FileInputFormat.setInputPaths(jobB, tmpPath);
        FileOutputFormat.setOutputPath(jobB, new Path(args[1]));

        jobB.setInputFormatClass(KeyValueTextInputFormat.class);
        jobB.setOutputFormatClass(TextOutputFormat.class);

        jobB.setJarByClass(PopularityLeague.class);
        return jobB.waitForCompletion(true) ? 0 : 1;
    }

    // TODO

    public static List<Integer> readHDFSFile(String path, Configuration conf) throws IOException{
        Path pt=new Path(path);
        FileSystem fs = FileSystem.get(pt.toUri(), conf);
        FSDataInputStream file = fs.open(pt);
        BufferedReader buffIn=new BufferedReader(new InputStreamReader(file));

        List<Integer> everything = new ArrayList<>();
        String line;
        while( (line = buffIn.readLine()) != null) {
            everything.add(Integer.valueOf(line));
        }
        return everything;
    }


    public static class LinkCountMap extends Mapper<Object, Text, IntWritable, IntWritable> {
        // TODO
        List<Integer> league;
        @Override
        protected void setup(Context context) throws IOException,InterruptedException {
            Configuration conf = context.getConfiguration();
            this.league = readHDFSFile(conf.get("league"), conf);
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            //TODO
            String line = value.toString();
            StringTokenizer t = new StringTokenizer(line, delimiters);
            Integer i = Integer.valueOf(t.nextToken());
            if(league.contains(i)) context.write(new IntWritable(i), new IntWritable(0));
            while(t.hasMoreTokens()){
                i = Integer.valueOf(t.nextToken());
                if(league.contains(i)) context.write(new IntWritable(i), new IntWritable(1));
            }
        }
    }

    public static class LinkCountReduce extends Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {
        // TODO
        @Override
        public void reduce(IntWritable key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            //TODO
            int sum = 0;
            for(IntWritable i: values){
                sum += i.get();
            }
            context.write(key, new IntWritable(sum));
        }
    }

    public static class LeagueRankMap extends Mapper<Text, Text, IntWritable, IntWritable> {
        Integer N;
        List<Pair<Integer,Integer>> l = new ArrayList<>();

        // TODO
        @Override
        public void map(Text key, Text value, Context context) throws IOException, InterruptedException {
            // TODO
            l.add(new Pair<>(Integer.valueOf(value.toString()),Integer.valueOf(key.toString())));
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // TODO
            Collections.sort(l, Collections.reverseOrder());
            for(int i=0;i<l.size();i++){
                int counter = 0;
                for(int j=i+1;j<l.size();j++){
                    if(l.get(j).first<l.get(i).first){
                        counter = l.size()-j;
                        break;
                    }
                }
                context.write(new IntWritable(l.get(i).second), new IntWritable(counter));
            }
        }
    }

    public static class LeagueRankReduce extends Reducer<IntWritable, IntWritable, IntWritable, IntWritable> {
        
    }
}

class Pair<A extends Comparable<? super A>,
        B extends Comparable<? super B>>
        implements Comparable<Pair<A, B>> {

    public final A first;
    public final B second;

    public Pair(A first, B second) {
        this.first = first;
        this.second = second;
    }

    public static <A extends Comparable<? super A>,
            B extends Comparable<? super B>>
    Pair<A, B> of(A first, B second) {
        return new Pair<A, B>(first, second);
    }

    @Override
    public int compareTo(Pair<A, B> o) {
        int cmp = o == null ? 1 : (this.first).compareTo(o.first);
        return cmp == 0 ? (this.second).compareTo(o.second) : cmp;
    }

    @Override
    public int hashCode() {
        return 31 * hashcode(first) + hashcode(second);
    }

    private static int hashcode(Object o) {
        return o == null ? 0 : o.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Pair))
            return false;
        if (this == obj)
            return true;
        return equal(first, ((Pair<?, ?>) obj).first)
                && equal(second, ((Pair<?, ?>) obj).second);
    }

    private boolean equal(Object o1, Object o2) {
        return o1 == o2 || (o1 != null && o1.equals(o2));
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ')';
    }
}
