library(caret)
library(car)
library(MASS)
library(gvlma)
load("/project/msca/capstone3/patient_matrix2revise.RData")

#Test and Train for Multicollinearity on numerical variables
set.seed(10)
df<-patient_matrix
df$patient_id<-NULL
df$mode_discharge_dispo<-NULL
df$sex<-NULL
df$race<-NULL
df$ethnicity<-NULL
df$marital_status<-NULL
train<-createDataPartition(df$y2_charges, p=0.7, list=FALSE)
#train<- sample(1:nrow(df),floor(0.7*nrow(df)))
training_set <- df[train,]
testing_set <- df[-train,]

#Multicollinearity of numeric variables and removal of highly correlated variables
zv<-apply(training_set,2,function(x) length(unique(x))==1) #code for zero variance predictors
sum(zv)
training_set<-training_set[,!zv] #remove zero variance predictors

ncol(training_set)
dfCorr<-cor(training_set) #build correlation matrix
highCorr<-findCorrelation(dfCorr, 0.7) #find all pairwise correlations greater than 0.7
traindf<-training_set[,-highCorr] #remove highly correlated predictors from training set
testdf<-testing_set[,-highCorr] #remove highly correlated predictors from testing set
ncol(traindf)
ncol(testdf)

#Build benchmark linear regression from reduced data and look at residuals to determine if further transformations are needed
fulllm<-lm(y2_charges~., data=traindf)
summary(fulllm)
fulllm.MSE <- mean((fulllm$fitted.values - traindf$y2_charges)^2)
fulllm.MSE

#Look at Residuals and outliers
outlierTest(fulllm)
#qqPlot(fulllm)
#leveragePlot(fulllm)
#av.Plots(fulllm)
sresid<-studres(fulllm)
hist(sresid, freq=FALSE)
curve(dnorm, add=TRUE)
#non-constant error variance test
ncvTest(fulllm)
#Non-constant Variance Score Test 
#Variance formula: ~ fitted.values 
#Chisquare = 141101.3    Df = 1     p = 0 #data is significantly heteroscedastic
spreadLevelPlot(fulllm)
plot(density(resid(fulllm)))
y2_charges.res<-resid(fulllm)
plot(y2_charges.res)
abline(0,0)
y2_charges.stdres<-rstandard(fulllm)
qqnorm(y2_charges.stdres)
qqline(y2_charges.stdres)
hist(resid(fulllm))

#Evaluate nonlinearity
#crPlots(fulllm)
ceresPlots(fulllm)
#Test for Autocorrelated Errors
durbinWatsonTest(fulllm)
#Global test for model assumptions
gvmodel<-gvlma(fulllm)
summary(gvmodel)
#Predict on test set
pred<-predict(fulllm, newdata=testdf)
summary(pred)

#Look at predictors correlation to Y

#Perform necessary transformations

#Perform PCA
set.seed(10)
xTrans<-preProcess(traindf, method=c("center","scale", "pca"))

xtrain<-predict(xTrans, traindf)
xtest<-predict(xTrans, testdf)

#re-run linear models

#Machine learning models
#regression tree
dftree<-patient_matrix
dftree$patient_id<-NULL
set.seed(10)
traintree <- createDataPartition(df$y2_charges, p=0.7, list=FALSE)
training_set_tree <- dftree[traintree,]
testing_set_tree <- dftree[-traintree,]
bootControl<-trainControl(number=200)



#str(dftree)




regression.tree<-tree(y2_charges~.,dftree,subset=traintree)
summary(regression.tree)

plot(regression.tree)
text(regression.tree,pretty=0)
cv.regressiontree=cv.tree(regression.tree)
plot(cv.regressiontree$size,cv.regressiontree$dev,type="b")

prune.regressiontree=prune.tree(regression.tree,best=7)
plot(prune.regressiontree)
text(prune.regressiontree,pretty=0)

yhat=predict(regression.tree,newdata=testing_set_tree)
regression.test=dftree[-traintree, "y2_charges"]
plot(yhat,regression.test)
abline(0,1)
mean((yhat-regression.test)^2)

