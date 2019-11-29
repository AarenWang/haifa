package org.wrj.haifa.process;

import java.io.*;

public class ExecuteCommand {

    public static void main(String[] args)  throws Exception {
        ExecuteCommand  executeCommand = new ExecuteCommand();
        System.out.println(System.getenv("HOME"));
        File file = new File(System.getenv("HOME")+"/"+"out.txt");
        if(!file.exists()){
            file.createNewFile();
        }
        executeCommand.executeCommand(args,file);
    }

    public void executeCommand(String[] commands, File outputFile) throws IOException,InterruptedException {

        if(commands.length == 0 ){
            System.out.println("请输入要执行命令");
            return;
        }
        System.out.println("执行命令:"+String.join(",",commands));
        FileWriter fileWriter = null;
        try{
            fileWriter = new FileWriter(outputFile);
            ProcessBuilder build = new ProcessBuilder(commands);
            build.redirectErrorStream();

            Process process = build.start();
            InputStream inputStream = process.getInputStream();
            writeProcessOutToResponse(inputStream,fileWriter);
            process.waitFor();

            // success=0表示命令执行成功，1表示命令执行失败
            int resultCode = process.exitValue();
            System.out.println("执行返回码:"+resultCode);
            process.destroy();
        }finally {
            fileWriter.close();
        }






    }


    private void writeProcessOutToResponse(InputStream inputStream, FileWriter fileWriter) throws IOException{
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(isr);
        String line = null;
        while ((line = br.readLine()) != null){
            fileWriter.write(line+"\r\n");
        }
    }
}
