library(ggplot2)
library(grid)
library(gridExtra)


#!/usr/bin/Rscript
args = commandArgs(trailingOnly=TRUE)
if(length(args) != 3) {
  scriptname = substr(grep("^--file=",commandArgs(),value=TRUE),8,1000)
  cat("Usage: Rscript --vanilla",scriptname,"LogFileSummary PredictorNamesFile figureTargetPath.pdf\n")
  quit(status=1)
}

preLogFile = args[1]
preNamesFile = args[2]
pdf_file = args[3]

# assign the variable names to the three filename arguments passed with this Rscript

logData = read.delim(preLogFile, header=TRUE)
preNames = read.delim(preNamesFile, header=FALSE)
stats <- c(rep('I',nrow(preNames)),rep('C',nrow(preNames)))
all.data <- cbind(logData[,1:6],preNames,stats)
names(all.data) <- c('Mean','SE','Median','HPD_Low','HPD_Upp','ESS','Predictor','Var')

prior.odds <- ((1-0.5^(1/nrow(preNames)))/(1-(1-0.5^(1/nrow(preNames)))))
bf.3 <- 3*prior.odds / (1 + 3*prior.odds)

# CREATE INCLUSION PROBABILITY GRAPH
incl.probs <- ggplot(subset(all.data,Var=='I'),aes(x=Predictor,y=Mean,fill=Predictor)) + 
  geom_hline(yintercept = bf.3,linetype=3)+
  geom_bar(stat="identity",colour="black",position=position_dodge(0.9))+
  labs(y="Inclusion Probability",x="Predictor")+
  scale_y_continuous(breaks=c(0,0.2,0.4,0.6,0.8,1.0))+
  theme(legend.position="none",
        axis.title.x = element_text(size=8),
        axis.title.y = element_text(size=8),
        axis.text.y = element_text(size=6),
        axis.text.x = element_text(size=6,angle=45,hjust=1),
        panel.grid.major = element_blank(), 
        panel.grid.minor = element_blank(),
        panel.background = element_rect(fill=NA, color="black"), 
        panel.border = element_rect(colour = "black",fill=NA),
        strip.background = element_blank())

# CREATE REGRESSION COEFFICIENT GRAPH 
reg.coeffs <- ggplot(subset(all.data,Var=='C'),aes(x=Predictor,y=Mean,fill=Predictor)) + 
  geom_hline(yintercept=0,linetype=1,size=0.5) +
  geom_errorbar(aes(ymin=HPD_Low,ymax=HPD_Upp),width=0.4,position=position_dodge(0.9)) +
  geom_point(stat="identity",pch=21,position=position_dodge(0.9))+
  labs(y="ln (Regression Coefficient)",x="Predictor")+
  theme(legend.position="none",
        axis.title.x = element_blank(),
        axis.title.y = element_text(size=8),
        axis.text.y = element_text(size=6),
        axis.text.x = element_blank(),
        panel.grid.major = element_blank(), 
        panel.grid.minor = element_blank(),
        panel.background = element_rect(fill=NA, color="black"), 
        panel.border = element_rect(colour = "black",fill=NA),
        strip.background = element_blank())

pdf(pdf_file, width=7, height=5)
grid.arrange(reg.coeffs,incl.probs,nrow=2)
invisible(dev.off())