#!/usr/bin/python
#coding=utf-8

import os
import re
import sys
#import getpass
from commands import *
import commands
from time import *
#import pexpect
#import smtplib
#import MySQLdb
#from email.header import Header
#from email.MIMEText import MIMEText
sys.path.append('/local/int_jenkins/lib')
sys.path.append('/local/int_jenkins/mtk_patch/lib')
import xlrd
import string
from CheckFile import *
from dotProjectDb import *
from checkPatchInfor import *
from Config import *
sys.path.append('/local/int_jenkins/mtk_patch')
from Mangage import *

class PatchMerge(Mangage):
	def __init__(self,conf):
		self.simplex = False
		self.insertDataToPrsm= True
		basedir = conf.getConf('basedir','base dir','/local')
		self.proj = conf.getConf('project','project name','')
		self.eservice_ID = conf.getConf('eservice_ID','eservice ID',-1)
		self.force = conf.getConf('force','re cherry-pick forcely',False)
		self.repopath = '/local/tools/repo/repo'
		print 'change dir ==================>%s\n' % basedir
		os.chdir(basedir)
		self.__baseDir = os.getcwd()		
		self.debug = False
		self.__pushCodeToGit = True
		
		wb = xlrd.open_workbook('/local/int_jenkins/mtk_patch/jb3-mp-import.xls')
		mtkSheet = wb.sheet_by_name(u'ModemInfo')
		print "project name",self.proj
		for row in xrange(0,mtkSheet.nrows):
			if mtkSheet.cell(row,0).value.strip() == self.proj:
				mp_row = row
		
		if not mp_row:
			print "Cannot find project in the MOLY SHEET OF THE Excel!"
			sys.exit(1)
		
		self.__branch = mtkSheet.cell(mp_row, 1).value.strip()
		self.__project = mtkSheet.cell(mp_row, 2).value.strip()
		
		self.mtkproj = mtkSheet.cell(mp_row, 2).value.strip()
		self.mtkrelease = mtkSheet.cell(mp_row, 4).value.strip()
		self.__DriveOnlyBranch = mtkSheet.cell(mp_row, 12).value.strip().split(',')
		self.manifestdir = mtkSheet.cell(mp_row, 16).value.strip()
		self.prj_SIXTH_name = mtkSheet.cell(mp_row, 26).value.strip()
		self.patch_type_str = mtkSheet.cell(mp_row, 30).value.strip()
		if self.patch_type_str == '':
			self.patch_type_str = conf.getConf('patchtypestr','patch type str','SIXTH')
		print "self.prj_SIXTH_name",self.prj_SIXTH_name
		if self.prj_SIXTH_name:
			self.Code = "/local/mtk_patch_import/" + self.__branch +'/'+ self.prj_SIXTH_name
		else:
			self.Code = "/local/mtk_patch_import/" + self.__branch + "/modem"
		if not os.path.exists(self.Code + '/.git'):
			print self.patch_type_str
			tmp = getoutput("find %s -name '.git' | grep '%s' | sed 's/\/\.git//g'" % (self.Code,self.patch_type_str))
			if tmp:
				self.Code = tmp
				print 'tmp',tmp
				#print 'self.Code',self.Code
			else:
				tmp = getoutput("find %s -name '.git' | sed 's/\/\.git//g'" % (self.Code)).split('\n')
				_tmp = getoutput("find %s -name '*%s*' " % (tmp,self.patch_type_str)).split('\n')
				if len(tmp)==1 and len(_tmp)==1:
					self.Code = tmp
				else:
					print "no SIXTH modem found!"
					sys.exit(1)
		else:
			tmp = getoutput("find %s -name '*%s*'" % (self.Code,self.patch_type_str))
			if tmp:
				self.Code = tmp
		print 'self.Code',self.Code
		self.mergeDone = "/local/mtk_patch_import/mergeDone"
		self.__ignorePatch = "/local/mtk_patch_import/ignorePatch"
		self.ongoingPatch = "/local/mtk_patch_import/TODO"
		self.__mailDir = "/local/mtk_patch_import/Mail"
		self.__start = "/local/mtk_patch_import/start"
		self.__DriveOnlyCode = "/local/mtk_patch_import/"
		self.devCodeBranch = []
		self.devCodeProjectIDList = []
		
		self.importIdDict = {}
		self.projectid_codeBranch_Dict = {}
		self.patch_type = ''
		self.vnum = ''
		self.pnum = ''
		self.eservice_ID_pl = ''
		self.description = ''
		self.untardir = ''
		self.spacelimit = 5
		self.untarstr = "%s/MD-P%s"
		print self.Code,self.Code + '/mtk_rel',self.Code + '/mcu/mtk_rel'
		self.get_db_connection()
		self.devCodeProjectIDList = self.getProjectIDFromImportBranch(self.__branch)
		print self.devCodeProjectIDList
		
		
		self.devCodeBranch = self.getDevBranchNameFromIProjectID(self.devCodeProjectIDList,self.projectid_codeBranch_Dict,self.__branch)

		self.checkFileDict = self.getAffectFileDict()
		self.projectList = self.checkProjectName()
		if not os.path.isdir(self.__start):
		    	os.system('mkdir -p %s' % self.__start)

		print "baseDir = %s\nbranch = %s\nmtkproj = %s\nmtkrelease = %s\nongoingPatch = %s\nDriveOnlyBranchName = %s" % (self.__baseDir,self.__branch, self.mtkproj,self.mtkrelease, self.ongoingPatch,self.__DriveOnlyBranch)
		

	def repo_code(self, codedir, BranchName):
		print "Now checking the modem code if exist,"
		if not os.path.isdir(codedir):
			os.system('mkdir -p %s' % codedir)
		print 'change dir ==================>%s\n' % codedir
		os.chdir(codedir)

		if os.path.exists(codedir + "/.git") == False and os.path.exists(os.path.dirname(codedir) + "/.git") == False:
			print "no modem found in %s" % codedir
			print "Please check the branch %s code have dowloaded sucessfully!"
			sys.exit(1)
			
        
	def MergePatch(self,patchtype,patchNo=-1):
		print "Please create file(start) on /local/mtk_patch_import directory. If not exist, the script will stop and exit."
		self.repo_code(self.Code, self.__branch)
		if (os.path.exists("/local/mtk_patch_import/start") == True):
			self.reset(self.Code,self.__branch)
			print "patchNo %s" % patchNo
			patchFilename = self.takePatch(self.Code, patchNo)
			if 'Nofile' == patchFilename:
				print "No p%s patch. Please copy the correct patch to TODO directory" % patchNo
				sys.exit(1)
			os.chdir(self.Code)
			self.patch_type,self.vnum,self.pnum,self.eservice_ID_pl = self.getMtkPatchInfor(patchFilename)
			self.description = self.getDescriptionFromPatchListFile(self.Code, self.untardir,self.eservice_ID_pl)
			if self.eservice_ID==-1:
				self.eservice_ID = self.eservice_ID_pl
			os.chdir(self.Code)
			print "description",self.description 	        
			self.insertAllImportInfoTO_importSheet(self.devCodeProjectIDList,self.__branch,self.patch_type,self.vnum,self.pnum,self.eservice_ID,self.description,patchtype)
			comment = 'porting P%s_%s' %(patchNo, patchFilename.replace('(', '_').replace(')', '_'))
			print "commit comment is %s" % comment
			print comment
			if True == self.__pushCodeToGit:
				print 'start merge.....'
				gitpushlog = '/tmp/%s_gitpush.log'%self.__branch
				os.system(' > %s' % gitpushlog)
				os.system('git add .')
				os.system('git commit -m "%s"; git push jgs HEAD:%s 2>&1 | tee -a %s' % (comment,self.__branch,gitpushlog))
				tmp = getoutput("grep 'fatal:' %s" % gitpushlog)
				if tmp:
					print "push fatal error!"
					print "=========logs======"
					print tmp
					print "=========logs======"
					sys.exit(1)
				tmp = getoutput("grep 'error:' %s" % gitpushlog)
				if tmp:
					print "push error!"
					print "=========logs======"
					print tmp
					print "=========logs======"
					sys.exit(1)
				print "finish push to origin master"
				print 'finish merge....'
				self.del_todo_tmp_dir()
			else:
				print "The code build failed"
				sys.exit(1)
	

	def MergePatchToDriveOnly(self,patchNo=-1):
		for branch in self.__DriveOnlyBranch:
			DriveOnlyCode =  self.__DriveOnlyCode + branch
			DriveCode = DriveOnlyCode + "/modem"
			if not os.path.exists(DriveCode + '/.git'):
				tmp = getoutput("find %s -name '.git' | grep '%s' | sed 's/\/\.git//g'" % (DriveCode,self.patch_type_str))
				if tmp:
					DriveCode = tmp
					print 'tmp',tmp
					print 'DriveCode',DriveCode
				else:
					tmp = getoutput("find %s -name '.git' | sed 's/\/\.git//g'" % (DriveCode)).split('\n')
					_tmp = getoutput("find %s -name '*%s*' " % (tmp,self.patch_type_str)).split('\n')
					if len(tmp)==1 and len(_tmp)==1:
						DriveCode = tmp
					else:
						print "no SIXTH modem found!"
						sys.exit(1)
			else:
				tmp = getoutput("find %s -name '*%s*'" % (DriveCode,self.patch_type_str))
				if tmp:
					DriveCode = tmp
			print 'DriveCode',DriveCode
			self.repo_code(DriveCode,branch) 
			self.eachDriveOnlyBranchImport(DriveCode, branch, patchNo)
			DriveOnlyCode = ''
			DriveCode = ''		            	
		#self.movePatchToMergeDone(patchNo)
		self.del_todo_tmp_dir()

	def eachDriveOnlyBranchImport(self, DriveCode, eachbranch, patchNo=-1):
		print "Please create file(start) on /local/mtk_patch_import directory. If not exist, the script will stop and exit."  
		if (os.path.exists("/local/mtk_patch_import/start") == True):
			self.reset(DriveCode,eachbranch)
			print "patchNo %s" % patchNo
			patchFilename = self.takePatch(DriveCode,patchNo)
			print 'change dir ==================>%s\n' % DriveCode
			os.chdir(DriveCode)
			comment = 'porting P%s_%s for driveonly' %(patchNo, patchFilename.replace('(', '_').replace(')', '_'))
			print "commit comment is %s" % comment
			cherrypick_current = True
			if not self.force:
				print 'git log jgs/%s --format=%s | grep "%s" | grep "%s" | grep "porting P%s_" | sort' %(eachbranch,'%s^^^^^^%H^^^^^^$PWD', self.mtkproj,self.mtkrelease, patchNo)
				patchList = getoutput('git log jgs/%s --format=%s | grep "%s" | grep "%s" | grep "porting P%s_" | sort' %(eachbranch,'%s^^^^^^%H^^^^^^$PWD', self.mtkproj,self.mtkrelease, patchNo)).split('\n')
				print 'patchList========%s'%patchList
				for patch in patchList:
					if patch:
						cherrypick_current = False
			print 'cherrypick_current',cherrypick_current
			if True == self.__pushCodeToGit and cherrypick_current:
				print 'start merge.....'
				#self.takeImportCommitForDriveOnly(DriveCode, self.__branch, eachbranch, patchNo)
				gitpushlog = '/tmp/%s_gitpush.log'%self.__branch
				os.system(' > %s'%gitpushlog)
				os.system('git add .')
				os.system('git commit -m "%s";git push jgs HEAD:%s 2>&1 | tee -a %s' % (comment,eachbranch,gitpushlog))
				tmp = getoutput("grep 'fatal:' %s" % gitpushlog)
				if tmp:
					print "push fatal error!"
					print "=========logs======"
					print tmp
					print "=========logs======"
					#sys.exit(1)
				tmp = getoutput("grep 'error:' %s" % gitpushlog)
				if tmp:
					print "push error!"
					print "=========logs======"
					print tmp
					print "=========logs======"
					sys.exit(1)
			print 'finish drive only branch merge....'
			
    
                
	def reset(self, codedir,branch=''):
        	if False == self.debug:
			print "-----------------------------reset start-----------------------------\n"
			print 'change dir ==================>%s\n' % codedir
		    	os.chdir(codedir)
		    	#sleep(120)
		    	print "reset start... \nClean all, reset to HEAD and git pull"
			if os.path.isdir('./build'):
		    		os.system('rm -rf ./build')
		    	elif os.path.isdir('./mcu/build'):
				os.system('rm -rf ./mcu/build')
			else:
				print "No build was found during reseting the code"
				
			os.system('git reset --hard HEAD')
		    	os.system('git clean -df')
			print 'change dir ==================>%s\n' % '/local/mtk_patch_import/'+branch
			os.chdir('/local/mtk_patch_import/'+branch)
			tmpstrs = getoutput('grep modem .repo/project.list').split('\n')
			print "tmpstrs",tmpstrs
			sync_modem_str = ''
			if len(tmpstrs) == 1 :
				sync_modem_str = tmpstrs[0]
		    		
			elif len(tmpstrs) >1 :
				for tmpstr in tmpstrs:
					if tmpstr.find(self.patch_type_str) != -1:
						sync_modem_str = tmpstr
					elif self.prj_SIXTH_name and tmpstr.find(self.prj_SIXTH_name) != -1:
						sync_modem_str = tmpstr
					else:
						continue
			else:
				print 'modem no found in project!!!'
				sys.exit(1)
			if sync_modem_str:
				print '%s sync %s'%(self.repopath,sync_modem_str)
				os.system('%s sync %s'%(self.repopath,sync_modem_str))	
			else:
				print 'sync_modem_str is NULL!!!'
				print 'modem no found in project!!!'
				sys.exit(1)
		    	print "-----------------------------reset done-----------------------------\n"

    


    

	

	

			
	

	

    

if __name__ == "__main__":
	print "test for argv"
	print sys.argv
	conf = Config()
	conf.addFromArg(sys.argv[1:])
	patchtype = conf.getConf('patchtype','patch type','')
	patchnum = conf.getConf('patchnum','patch number',-1)#sys.argv[3]
	operation = conf.getConf('operation','operation','')
	pm = PatchMerge(conf)
	#print dir(pm)
	print '====================begin of %s====================\n' % operation
	if operation == 'MergePatch':
		pm.MergePatch(patchtype,patchnum)
	elif operation == 'MergePatchToDriveOnly':  
		pm.MergePatchToDriveOnly(patchnum)
	else:
		print "ERROR eccour in operation!"
	print '====================end of %s====================\n' % operation
	
