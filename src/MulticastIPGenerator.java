public class MulticastIPGenerator {
    private int part1;
    private int part2;
    private int part3;
    private int part4;

    public MulticastIPGenerator(int part1, int part2, int part3, int part4){
        this.part1 = part1;
        this.part2 = part2;
        this.part3 = part3;
        this.part4 = part4;
    }

    public MulticastIPGenerator(){}

    public String generateIP(){
        if(part4<255){
            this.part4++;
            return new String(part1 + "." + part2 + "." + part3 +"." + part4);
        }
        else{
            if(part3<255){
                this.part3++;
                this.part4=0;
                return new String(part1 + "." + part2 + "." + part3 +"." + part4);
            }
            else{
                if(part2<255){
                    this.part2++;
                    this.part3=0;
                    this.part4=0;
                    return new String(part1 + "." + part2 + "." + part3 +"." + part4);
                }
                else{
                    if(part1<239){
                        this.part1++;
                        this.part2=0;
                        this.part3=0;
                        this.part4=0;
                        return new String(part1 + "." + part2 + "." + part3 +"." + part4);
                    }
                    else return "ERROR";
                }
            }
        }
    }

    public void setPart1(int part1) { this.part1 = part1; }
    public void setPart2(int part2) { this.part2 = part2; }
    public void setPart3(int part3) { this.part3 = part3; }
    public void setPart4(int part4) { this.part4 = part4; }

    public int getPart1() { return part1; }
    public int getPart2() { return part2; }
    public int getPart3() { return part3; }
    public int getPart4() { return part4; }
}
